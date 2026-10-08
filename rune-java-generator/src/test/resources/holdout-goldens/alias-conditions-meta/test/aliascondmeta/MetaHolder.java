package test.aliascondmeta;

import com.google.common.collect.ImmutableList;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Multi;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.model.metafields.FieldWithMetaInteger;
import com.rosetta.util.ListEquals;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import test.aliascondmeta.meta.MetaHolderMeta;

import static java.util.Optional.ofNullable;

/**
 * Conditioned-alias attributes under [metadata scheme], single and multi.
 * @version 0.0.0
 */
@RosettaDataType(value="MetaHolder", builder=MetaHolder.MetaHolderBuilderImpl.class, version="0.0.0")
@RuneDataType(value="MetaHolder", model="test", builder=MetaHolder.MetaHolderBuilderImpl.class, version="0.0.0")
public interface MetaHolder extends RosettaModelObject {

	MetaHolderMeta metaData = new MetaHolderMeta();

	/*********************** Getter Methods  ***********************/
	FieldWithMetaInteger getSchemed();
	List<? extends FieldWithMetaInteger> getSchemeds();
	Integer getPlain();

	/*********************** Build Methods  ***********************/
	MetaHolder build();
	
	MetaHolder.MetaHolderBuilder toBuilder();
	
	static MetaHolder.MetaHolderBuilder builder() {
		return new MetaHolder.MetaHolderBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends MetaHolder> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends MetaHolder> getType() {
		return MetaHolder.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("schemed"), processor, FieldWithMetaInteger.class, getSchemed());
		processRosetta(path.newSubPath("schemeds"), processor, FieldWithMetaInteger.class, getSchemeds());
		processor.processBasic(path.newSubPath("plain"), Integer.class, getPlain(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface MetaHolderBuilder extends MetaHolder, RosettaModelObjectBuilder {
		FieldWithMetaInteger.FieldWithMetaIntegerBuilder getOrCreateSchemed();
		@Override
		FieldWithMetaInteger.FieldWithMetaIntegerBuilder getSchemed();
		FieldWithMetaInteger.FieldWithMetaIntegerBuilder getOrCreateSchemeds(int index);
		@Override
		List<? extends FieldWithMetaInteger.FieldWithMetaIntegerBuilder> getSchemeds();
		MetaHolder.MetaHolderBuilder setSchemed(FieldWithMetaInteger schemed);
		MetaHolder.MetaHolderBuilder setSchemedValue(Integer schemed);
		MetaHolder.MetaHolderBuilder addSchemeds(FieldWithMetaInteger schemeds);
		MetaHolder.MetaHolderBuilder addSchemeds(FieldWithMetaInteger schemeds, int idx);
		MetaHolder.MetaHolderBuilder addSchemedsValue(Integer schemeds);
		MetaHolder.MetaHolderBuilder addSchemedsValue(Integer schemeds, int idx);
		MetaHolder.MetaHolderBuilder addSchemeds(List<? extends FieldWithMetaInteger> schemeds);
		MetaHolder.MetaHolderBuilder setSchemeds(List<? extends FieldWithMetaInteger> schemeds);
		MetaHolder.MetaHolderBuilder addSchemedsValue(List<? extends Integer> schemeds);
		MetaHolder.MetaHolderBuilder setSchemedsValue(List<? extends Integer> schemeds);
		MetaHolder.MetaHolderBuilder setPlain(Integer plain);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("schemed"), processor, FieldWithMetaInteger.FieldWithMetaIntegerBuilder.class, getSchemed());
			processRosetta(path.newSubPath("schemeds"), processor, FieldWithMetaInteger.FieldWithMetaIntegerBuilder.class, getSchemeds());
			processor.processBasic(path.newSubPath("plain"), Integer.class, getPlain(), this);
		}
		

		MetaHolder.MetaHolderBuilder prune();
	}

	/*********************** Immutable Implementation of MetaHolder  ***********************/
	class MetaHolderImpl implements MetaHolder {
		private final FieldWithMetaInteger schemed;
		private final List<? extends FieldWithMetaInteger> schemeds;
		private final Integer plain;
		
		protected MetaHolderImpl(MetaHolder.MetaHolderBuilder builder) {
			this.schemed = ofNullable(builder.getSchemed()).map(f->f.build()).orElse(null);
			this.schemeds = ofNullable(builder.getSchemeds()).filter(_l->!_l.isEmpty()).map(list -> list.stream().filter(Objects::nonNull).map(f->f.build()).filter(Objects::nonNull).collect(ImmutableList.toImmutableList())).orElse(null);
			this.plain = builder.getPlain();
		}
		
		@Override
		@RosettaAttribute("schemed")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("schemed")
		public FieldWithMetaInteger getSchemed() {
			return schemed;
		}
		
		@Override
		@RosettaAttribute("schemeds")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("schemeds")
		public List<? extends FieldWithMetaInteger> getSchemeds() {
			return schemeds;
		}
		
		@Override
		@RosettaAttribute("plain")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("plain")
		public Integer getPlain() {
			return plain;
		}
		
		@Override
		public MetaHolder build() {
			return this;
		}
		
		@Override
		public MetaHolder.MetaHolderBuilder toBuilder() {
			MetaHolder.MetaHolderBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(MetaHolder.MetaHolderBuilder builder) {
			ofNullable(getSchemed()).ifPresent(builder::setSchemed);
			ofNullable(getSchemeds()).ifPresent(builder::setSchemeds);
			ofNullable(getPlain()).ifPresent(builder::setPlain);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			MetaHolder _that = getType().cast(o);
		
			if (!Objects.equals(schemed, _that.getSchemed())) return false;
			if (!ListEquals.listEquals(schemeds, _that.getSchemeds())) return false;
			if (!Objects.equals(plain, _that.getPlain())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (schemed != null ? schemed.hashCode() : 0);
			_result = 31 * _result + (schemeds != null ? schemeds.hashCode() : 0);
			_result = 31 * _result + (plain != null ? plain.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "MetaHolder {" +
				"schemed=" + this.schemed + ", " +
				"schemeds=" + this.schemeds + ", " +
				"plain=" + this.plain +
			'}';
		}
	}

	/*********************** Builder Implementation of MetaHolder  ***********************/
	class MetaHolderBuilderImpl implements MetaHolder.MetaHolderBuilder {
	
		protected FieldWithMetaInteger.FieldWithMetaIntegerBuilder schemed;
		protected List<FieldWithMetaInteger.FieldWithMetaIntegerBuilder> schemeds = new ArrayList<>();
		protected Integer plain;
		
		@Override
		@RosettaAttribute("schemed")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("schemed")
		public FieldWithMetaInteger.FieldWithMetaIntegerBuilder getSchemed() {
			return schemed;
		}
		
		@Override
		public FieldWithMetaInteger.FieldWithMetaIntegerBuilder getOrCreateSchemed() {
			FieldWithMetaInteger.FieldWithMetaIntegerBuilder result;
			if (schemed!=null) {
				result = schemed;
			}
			else {
				result = schemed = FieldWithMetaInteger.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("schemeds")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("schemeds")
		public List<? extends FieldWithMetaInteger.FieldWithMetaIntegerBuilder> getSchemeds() {
			return schemeds;
		}
		
		@Override
		public FieldWithMetaInteger.FieldWithMetaIntegerBuilder getOrCreateSchemeds(int index) {
			if (schemeds==null) {
				this.schemeds = new ArrayList<>();
			}
			return getIndex(schemeds, index, () -> {
						FieldWithMetaInteger.FieldWithMetaIntegerBuilder newSchemeds = FieldWithMetaInteger.builder();
						return newSchemeds;
					});
		}
		
		@Override
		@RosettaAttribute("plain")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("plain")
		public Integer getPlain() {
			return plain;
		}
		
		@RosettaAttribute("schemed")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("schemed")
		@Override
		public MetaHolder.MetaHolderBuilder setSchemed(FieldWithMetaInteger _schemed) {
			this.schemed = _schemed == null ? null : _schemed.toBuilder();
			return this;
		}
		
		@Override
		public MetaHolder.MetaHolderBuilder setSchemedValue(Integer _schemed) {
			this.getOrCreateSchemed().setValue(_schemed);
			return this;
		}
		
		@RosettaAttribute("schemeds")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("schemeds")
		@Override
		public MetaHolder.MetaHolderBuilder addSchemeds(FieldWithMetaInteger _schemeds) {
			if (_schemeds != null) {
				this.schemeds.add(_schemeds.toBuilder());
			}
			return this;
		}
		
		@Override
		public MetaHolder.MetaHolderBuilder addSchemeds(FieldWithMetaInteger _schemeds, int idx) {
			getIndex(this.schemeds, idx, () -> _schemeds.toBuilder());
			return this;
		}
		
		@Override
		public MetaHolder.MetaHolderBuilder addSchemedsValue(Integer _schemeds) {
			this.getOrCreateSchemeds(-1).setValue(_schemeds);
			return this;
		}
		
		@Override
		public MetaHolder.MetaHolderBuilder addSchemedsValue(Integer _schemeds, int idx) {
			this.getOrCreateSchemeds(idx).setValue(_schemeds);
			return this;
		}
		
		@Override
		public MetaHolder.MetaHolderBuilder addSchemeds(List<? extends FieldWithMetaInteger> schemedss) {
			if (schemedss != null) {
				for (final FieldWithMetaInteger toAdd : schemedss) {
					this.schemeds.add(toAdd.toBuilder());
				}
			}
			return this;
		}
		
		@RosettaAttribute("schemeds")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("schemeds")
		@Override
		public MetaHolder.MetaHolderBuilder setSchemeds(List<? extends FieldWithMetaInteger> schemedss) {
			if (schemedss == null) {
				this.schemeds = new ArrayList<>();
			} else {
				this.schemeds = schemedss.stream()
					.map(_a->_a.toBuilder())
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public MetaHolder.MetaHolderBuilder addSchemedsValue(List<? extends Integer> schemedss) {
			if (schemedss != null) {
				for (final Integer toAdd : schemedss) {
					this.addSchemedsValue(toAdd);
				}
			}
			return this;
		}
		
		@Override
		public MetaHolder.MetaHolderBuilder setSchemedsValue(List<? extends Integer> schemedss) {
			this.schemeds.clear();
			if (schemedss != null) {
				schemedss.forEach(this::addSchemedsValue);
			}
			return this;
		}
		
		@RosettaAttribute("plain")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("plain")
		@Override
		public MetaHolder.MetaHolderBuilder setPlain(Integer _plain) {
			this.plain = _plain == null ? null : _plain;
			return this;
		}
		
		@Override
		public MetaHolder build() {
			return new MetaHolder.MetaHolderImpl(this);
		}
		
		@Override
		public MetaHolder.MetaHolderBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public MetaHolder.MetaHolderBuilder prune() {
			if (schemed!=null && !schemed.prune().hasData()) schemed = null;
			schemeds = schemeds.stream().filter(b->b!=null).<FieldWithMetaInteger.FieldWithMetaIntegerBuilder>map(b->b.prune()).filter(b->b.hasData()).collect(Collectors.toList());
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getSchemed()!=null) return true;
			if (getSchemeds()!=null && !getSchemeds().isEmpty()) return true;
			if (getPlain()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public MetaHolder.MetaHolderBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			MetaHolder.MetaHolderBuilder o = (MetaHolder.MetaHolderBuilder) other;
			
			merger.mergeRosetta(getSchemed(), o.getSchemed(), this::setSchemed);
			merger.mergeRosetta(getSchemeds(), o.getSchemeds(), this::getOrCreateSchemeds);
			
			merger.mergeBasic(getPlain(), o.getPlain(), this::setPlain);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			MetaHolder _that = getType().cast(o);
		
			if (!Objects.equals(schemed, _that.getSchemed())) return false;
			if (!ListEquals.listEquals(schemeds, _that.getSchemeds())) return false;
			if (!Objects.equals(plain, _that.getPlain())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (schemed != null ? schemed.hashCode() : 0);
			_result = 31 * _result + (schemeds != null ? schemeds.hashCode() : 0);
			_result = 31 * _result + (plain != null ? plain.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "MetaHolderBuilder {" +
				"schemed=" + this.schemed + ", " +
				"schemeds=" + this.schemeds + ", " +
				"plain=" + this.plain +
			'}';
		}
	}
}
