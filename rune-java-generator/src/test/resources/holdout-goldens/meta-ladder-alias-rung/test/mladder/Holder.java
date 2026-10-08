package test.mladder;

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
import com.rosetta.model.metafields.FieldWithMetaString;
import com.rosetta.util.ListEquals;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import test.mladder.meta.HolderMeta;
import test.mladder.metafields.ReferenceWithMetaKeyed;

import static java.util.Optional.ofNullable;

/**
 * The ladder&#39;s sources (the chaos C9Holder).
 * @version 0.0.0
 */
@RosettaDataType(value="Holder", builder=Holder.HolderBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Holder", model="test", builder=Holder.HolderBuilderImpl.class, version="0.0.0")
public interface Holder extends RosettaModelObject {

	HolderMeta metaData = new HolderMeta();

	/*********************** Getter Methods  ***********************/
	FieldWithMetaString getCoded();
	List<? extends FieldWithMetaString> getCodes();
	List<? extends ReferenceWithMetaKeyed> getByRefs();

	/*********************** Build Methods  ***********************/
	Holder build();
	
	Holder.HolderBuilder toBuilder();
	
	static Holder.HolderBuilder builder() {
		return new Holder.HolderBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Holder> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Holder> getType() {
		return Holder.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("coded"), processor, FieldWithMetaString.class, getCoded());
		processRosetta(path.newSubPath("codes"), processor, FieldWithMetaString.class, getCodes());
		processRosetta(path.newSubPath("byRefs"), processor, ReferenceWithMetaKeyed.class, getByRefs());
	}
	

	/*********************** Builder Interface  ***********************/
	interface HolderBuilder extends Holder, RosettaModelObjectBuilder {
		FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateCoded();
		@Override
		FieldWithMetaString.FieldWithMetaStringBuilder getCoded();
		FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateCodes(int index);
		@Override
		List<? extends FieldWithMetaString.FieldWithMetaStringBuilder> getCodes();
		ReferenceWithMetaKeyed.ReferenceWithMetaKeyedBuilder getOrCreateByRefs(int index);
		@Override
		List<? extends ReferenceWithMetaKeyed.ReferenceWithMetaKeyedBuilder> getByRefs();
		Holder.HolderBuilder setCoded(FieldWithMetaString coded);
		Holder.HolderBuilder setCodedValue(String coded);
		Holder.HolderBuilder addCodes(FieldWithMetaString codes);
		Holder.HolderBuilder addCodes(FieldWithMetaString codes, int idx);
		Holder.HolderBuilder addCodesValue(String codes);
		Holder.HolderBuilder addCodesValue(String codes, int idx);
		Holder.HolderBuilder addCodes(List<? extends FieldWithMetaString> codes);
		Holder.HolderBuilder setCodes(List<? extends FieldWithMetaString> codes);
		Holder.HolderBuilder addCodesValue(List<? extends String> codes);
		Holder.HolderBuilder setCodesValue(List<? extends String> codes);
		Holder.HolderBuilder addByRefs(ReferenceWithMetaKeyed byRefs);
		Holder.HolderBuilder addByRefs(ReferenceWithMetaKeyed byRefs, int idx);
		Holder.HolderBuilder addByRefsValue(Keyed byRefs);
		Holder.HolderBuilder addByRefsValue(Keyed byRefs, int idx);
		Holder.HolderBuilder addByRefs(List<? extends ReferenceWithMetaKeyed> byRefs);
		Holder.HolderBuilder setByRefs(List<? extends ReferenceWithMetaKeyed> byRefs);
		Holder.HolderBuilder addByRefsValue(List<? extends Keyed> byRefs);
		Holder.HolderBuilder setByRefsValue(List<? extends Keyed> byRefs);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("coded"), processor, FieldWithMetaString.FieldWithMetaStringBuilder.class, getCoded());
			processRosetta(path.newSubPath("codes"), processor, FieldWithMetaString.FieldWithMetaStringBuilder.class, getCodes());
			processRosetta(path.newSubPath("byRefs"), processor, ReferenceWithMetaKeyed.ReferenceWithMetaKeyedBuilder.class, getByRefs());
		}
		

		Holder.HolderBuilder prune();
	}

	/*********************** Immutable Implementation of Holder  ***********************/
	class HolderImpl implements Holder {
		private final FieldWithMetaString coded;
		private final List<? extends FieldWithMetaString> codes;
		private final List<? extends ReferenceWithMetaKeyed> byRefs;
		
		protected HolderImpl(Holder.HolderBuilder builder) {
			this.coded = ofNullable(builder.getCoded()).map(f->f.build()).orElse(null);
			this.codes = ofNullable(builder.getCodes()).filter(_l->!_l.isEmpty()).map(list -> list.stream().filter(Objects::nonNull).map(f->f.build()).filter(Objects::nonNull).collect(ImmutableList.toImmutableList())).orElse(null);
			this.byRefs = ofNullable(builder.getByRefs()).filter(_l->!_l.isEmpty()).map(list -> list.stream().filter(Objects::nonNull).map(f->f.build()).filter(Objects::nonNull).collect(ImmutableList.toImmutableList())).orElse(null);
		}
		
		@Override
		@RosettaAttribute("coded")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("coded")
		public FieldWithMetaString getCoded() {
			return coded;
		}
		
		@Override
		@RosettaAttribute("codes")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("codes")
		public List<? extends FieldWithMetaString> getCodes() {
			return codes;
		}
		
		@Override
		@RosettaAttribute("byRefs")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("byRefs")
		public List<? extends ReferenceWithMetaKeyed> getByRefs() {
			return byRefs;
		}
		
		@Override
		public Holder build() {
			return this;
		}
		
		@Override
		public Holder.HolderBuilder toBuilder() {
			Holder.HolderBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Holder.HolderBuilder builder) {
			ofNullable(getCoded()).ifPresent(builder::setCoded);
			ofNullable(getCodes()).ifPresent(builder::setCodes);
			ofNullable(getByRefs()).ifPresent(builder::setByRefs);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Holder _that = getType().cast(o);
		
			if (!Objects.equals(coded, _that.getCoded())) return false;
			if (!ListEquals.listEquals(codes, _that.getCodes())) return false;
			if (!ListEquals.listEquals(byRefs, _that.getByRefs())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (coded != null ? coded.hashCode() : 0);
			_result = 31 * _result + (codes != null ? codes.hashCode() : 0);
			_result = 31 * _result + (byRefs != null ? byRefs.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Holder {" +
				"coded=" + this.coded + ", " +
				"codes=" + this.codes + ", " +
				"byRefs=" + this.byRefs +
			'}';
		}
	}

	/*********************** Builder Implementation of Holder  ***********************/
	class HolderBuilderImpl implements Holder.HolderBuilder {
	
		protected FieldWithMetaString.FieldWithMetaStringBuilder coded;
		protected List<FieldWithMetaString.FieldWithMetaStringBuilder> codes = new ArrayList<>();
		protected List<ReferenceWithMetaKeyed.ReferenceWithMetaKeyedBuilder> byRefs = new ArrayList<>();
		
		@Override
		@RosettaAttribute("coded")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("coded")
		public FieldWithMetaString.FieldWithMetaStringBuilder getCoded() {
			return coded;
		}
		
		@Override
		public FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateCoded() {
			FieldWithMetaString.FieldWithMetaStringBuilder result;
			if (coded!=null) {
				result = coded;
			}
			else {
				result = coded = FieldWithMetaString.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("codes")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("codes")
		public List<? extends FieldWithMetaString.FieldWithMetaStringBuilder> getCodes() {
			return codes;
		}
		
		@Override
		public FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateCodes(int index) {
			if (codes==null) {
				this.codes = new ArrayList<>();
			}
			return getIndex(codes, index, () -> {
						FieldWithMetaString.FieldWithMetaStringBuilder newCodes = FieldWithMetaString.builder();
						return newCodes;
					});
		}
		
		@Override
		@RosettaAttribute("byRefs")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("byRefs")
		public List<? extends ReferenceWithMetaKeyed.ReferenceWithMetaKeyedBuilder> getByRefs() {
			return byRefs;
		}
		
		@Override
		public ReferenceWithMetaKeyed.ReferenceWithMetaKeyedBuilder getOrCreateByRefs(int index) {
			if (byRefs==null) {
				this.byRefs = new ArrayList<>();
			}
			return getIndex(byRefs, index, () -> {
						ReferenceWithMetaKeyed.ReferenceWithMetaKeyedBuilder newByRefs = ReferenceWithMetaKeyed.builder();
						return newByRefs;
					});
		}
		
		@RosettaAttribute("coded")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("coded")
		@Override
		public Holder.HolderBuilder setCoded(FieldWithMetaString _coded) {
			this.coded = _coded == null ? null : _coded.toBuilder();
			return this;
		}
		
		@Override
		public Holder.HolderBuilder setCodedValue(String _coded) {
			this.getOrCreateCoded().setValue(_coded);
			return this;
		}
		
		@RosettaAttribute("codes")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("codes")
		@Override
		public Holder.HolderBuilder addCodes(FieldWithMetaString _codes) {
			if (_codes != null) {
				this.codes.add(_codes.toBuilder());
			}
			return this;
		}
		
		@Override
		public Holder.HolderBuilder addCodes(FieldWithMetaString _codes, int idx) {
			getIndex(this.codes, idx, () -> _codes.toBuilder());
			return this;
		}
		
		@Override
		public Holder.HolderBuilder addCodesValue(String _codes) {
			this.getOrCreateCodes(-1).setValue(_codes);
			return this;
		}
		
		@Override
		public Holder.HolderBuilder addCodesValue(String _codes, int idx) {
			this.getOrCreateCodes(idx).setValue(_codes);
			return this;
		}
		
		@Override
		public Holder.HolderBuilder addCodes(List<? extends FieldWithMetaString> codess) {
			if (codess != null) {
				for (final FieldWithMetaString toAdd : codess) {
					this.codes.add(toAdd.toBuilder());
				}
			}
			return this;
		}
		
		@RosettaAttribute("codes")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("codes")
		@Override
		public Holder.HolderBuilder setCodes(List<? extends FieldWithMetaString> codess) {
			if (codess == null) {
				this.codes = new ArrayList<>();
			} else {
				this.codes = codess.stream()
					.map(_a->_a.toBuilder())
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public Holder.HolderBuilder addCodesValue(List<? extends String> codess) {
			if (codess != null) {
				for (final String toAdd : codess) {
					this.addCodesValue(toAdd);
				}
			}
			return this;
		}
		
		@Override
		public Holder.HolderBuilder setCodesValue(List<? extends String> codess) {
			this.codes.clear();
			if (codess != null) {
				codess.forEach(this::addCodesValue);
			}
			return this;
		}
		
		@RosettaAttribute("byRefs")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("byRefs")
		@Override
		public Holder.HolderBuilder addByRefs(ReferenceWithMetaKeyed _byRefs) {
			if (_byRefs != null) {
				this.byRefs.add(_byRefs.toBuilder());
			}
			return this;
		}
		
		@Override
		public Holder.HolderBuilder addByRefs(ReferenceWithMetaKeyed _byRefs, int idx) {
			getIndex(this.byRefs, idx, () -> _byRefs.toBuilder());
			return this;
		}
		
		@Override
		public Holder.HolderBuilder addByRefsValue(Keyed _byRefs) {
			this.getOrCreateByRefs(-1).setValue(_byRefs.toBuilder());
			return this;
		}
		
		@Override
		public Holder.HolderBuilder addByRefsValue(Keyed _byRefs, int idx) {
			this.getOrCreateByRefs(idx).setValue(_byRefs.toBuilder());
			return this;
		}
		
		@Override
		public Holder.HolderBuilder addByRefs(List<? extends ReferenceWithMetaKeyed> byRefss) {
			if (byRefss != null) {
				for (final ReferenceWithMetaKeyed toAdd : byRefss) {
					this.byRefs.add(toAdd.toBuilder());
				}
			}
			return this;
		}
		
		@RosettaAttribute("byRefs")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("byRefs")
		@Override
		public Holder.HolderBuilder setByRefs(List<? extends ReferenceWithMetaKeyed> byRefss) {
			if (byRefss == null) {
				this.byRefs = new ArrayList<>();
			} else {
				this.byRefs = byRefss.stream()
					.map(_a->_a.toBuilder())
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public Holder.HolderBuilder addByRefsValue(List<? extends Keyed> byRefss) {
			if (byRefss != null) {
				for (final Keyed toAdd : byRefss) {
					this.addByRefsValue(toAdd);
				}
			}
			return this;
		}
		
		@Override
		public Holder.HolderBuilder setByRefsValue(List<? extends Keyed> byRefss) {
			this.byRefs.clear();
			if (byRefss != null) {
				byRefss.forEach(this::addByRefsValue);
			}
			return this;
		}
		
		@Override
		public Holder build() {
			return new Holder.HolderImpl(this);
		}
		
		@Override
		public Holder.HolderBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Holder.HolderBuilder prune() {
			if (coded!=null && !coded.prune().hasData()) coded = null;
			codes = codes.stream().filter(b->b!=null).<FieldWithMetaString.FieldWithMetaStringBuilder>map(b->b.prune()).filter(b->b.hasData()).collect(Collectors.toList());
			byRefs = byRefs.stream().filter(b->b!=null).<ReferenceWithMetaKeyed.ReferenceWithMetaKeyedBuilder>map(b->b.prune()).filter(b->b.hasData()).collect(Collectors.toList());
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getCoded()!=null) return true;
			if (getCodes()!=null && !getCodes().isEmpty()) return true;
			if (getByRefs()!=null && getByRefs().stream().filter(Objects::nonNull).anyMatch(a->a.hasData())) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Holder.HolderBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Holder.HolderBuilder o = (Holder.HolderBuilder) other;
			
			merger.mergeRosetta(getCoded(), o.getCoded(), this::setCoded);
			merger.mergeRosetta(getCodes(), o.getCodes(), this::getOrCreateCodes);
			merger.mergeRosetta(getByRefs(), o.getByRefs(), this::getOrCreateByRefs);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Holder _that = getType().cast(o);
		
			if (!Objects.equals(coded, _that.getCoded())) return false;
			if (!ListEquals.listEquals(codes, _that.getCodes())) return false;
			if (!ListEquals.listEquals(byRefs, _that.getByRefs())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (coded != null ? coded.hashCode() : 0);
			_result = 31 * _result + (codes != null ? codes.hashCode() : 0);
			_result = 31 * _result + (byRefs != null ? byRefs.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "HolderBuilder {" +
				"coded=" + this.coded + ", " +
				"codes=" + this.codes + ", " +
				"byRefs=" + this.byRefs +
			'}';
		}
	}
}
