package test.wmwrap;

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
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.model.metafields.FieldWithMetaString;
import com.rosetta.util.ListEquals;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import test.wmwrap.meta.HolderMeta;

import static java.util.Optional.ofNullable;

/**
 * Attribute-level meta shapes (the chaos C9Holder).
 * @version 0.0.0
 */
@RosettaDataType(value="Holder", builder=Holder.HolderBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Holder", model="test", builder=Holder.HolderBuilderImpl.class, version="0.0.0")
public interface Holder extends RosettaModelObject {

	HolderMeta metaData = new HolderMeta();

	/*********************** Getter Methods  ***********************/
	FieldWithMetaString getCoded();
	List<? extends FieldWithMetaString> getCodes();
	FieldWithMetaString getMarked();
	String getPlain();

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
		processRosetta(path.newSubPath("marked"), processor, FieldWithMetaString.class, getMarked(), AttributeMeta.GLOBAL_KEY_FIELD);
		processor.processBasic(path.newSubPath("plain"), String.class, getPlain(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface HolderBuilder extends Holder, RosettaModelObjectBuilder {
		FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateCoded();
		@Override
		FieldWithMetaString.FieldWithMetaStringBuilder getCoded();
		FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateCodes(int index);
		@Override
		List<? extends FieldWithMetaString.FieldWithMetaStringBuilder> getCodes();
		FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateMarked();
		@Override
		FieldWithMetaString.FieldWithMetaStringBuilder getMarked();
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
		Holder.HolderBuilder setMarked(FieldWithMetaString marked);
		Holder.HolderBuilder setMarkedValue(String marked);
		Holder.HolderBuilder setPlain(String plain);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("coded"), processor, FieldWithMetaString.FieldWithMetaStringBuilder.class, getCoded());
			processRosetta(path.newSubPath("codes"), processor, FieldWithMetaString.FieldWithMetaStringBuilder.class, getCodes());
			processRosetta(path.newSubPath("marked"), processor, FieldWithMetaString.FieldWithMetaStringBuilder.class, getMarked(), AttributeMeta.GLOBAL_KEY_FIELD);
			processor.processBasic(path.newSubPath("plain"), String.class, getPlain(), this);
		}
		

		Holder.HolderBuilder prune();
	}

	/*********************** Immutable Implementation of Holder  ***********************/
	class HolderImpl implements Holder {
		private final FieldWithMetaString coded;
		private final List<? extends FieldWithMetaString> codes;
		private final FieldWithMetaString marked;
		private final String plain;
		
		protected HolderImpl(Holder.HolderBuilder builder) {
			this.coded = ofNullable(builder.getCoded()).map(f->f.build()).orElse(null);
			this.codes = ofNullable(builder.getCodes()).filter(_l->!_l.isEmpty()).map(list -> list.stream().filter(Objects::nonNull).map(f->f.build()).filter(Objects::nonNull).collect(ImmutableList.toImmutableList())).orElse(null);
			this.marked = ofNullable(builder.getMarked()).map(f->f.build()).orElse(null);
			this.plain = builder.getPlain();
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
		@RosettaAttribute("marked")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("marked")
		public FieldWithMetaString getMarked() {
			return marked;
		}
		
		@Override
		@RosettaAttribute("plain")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("plain")
		public String getPlain() {
			return plain;
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
			ofNullable(getMarked()).ifPresent(builder::setMarked);
			ofNullable(getPlain()).ifPresent(builder::setPlain);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Holder _that = getType().cast(o);
		
			if (!Objects.equals(coded, _that.getCoded())) return false;
			if (!ListEquals.listEquals(codes, _that.getCodes())) return false;
			if (!Objects.equals(marked, _that.getMarked())) return false;
			if (!Objects.equals(plain, _that.getPlain())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (coded != null ? coded.hashCode() : 0);
			_result = 31 * _result + (codes != null ? codes.hashCode() : 0);
			_result = 31 * _result + (marked != null ? marked.hashCode() : 0);
			_result = 31 * _result + (plain != null ? plain.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Holder {" +
				"coded=" + this.coded + ", " +
				"codes=" + this.codes + ", " +
				"marked=" + this.marked + ", " +
				"plain=" + this.plain +
			'}';
		}
	}

	/*********************** Builder Implementation of Holder  ***********************/
	class HolderBuilderImpl implements Holder.HolderBuilder {
	
		protected FieldWithMetaString.FieldWithMetaStringBuilder coded;
		protected List<FieldWithMetaString.FieldWithMetaStringBuilder> codes = new ArrayList<>();
		protected FieldWithMetaString.FieldWithMetaStringBuilder marked;
		protected String plain;
		
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
		@RosettaAttribute("marked")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("marked")
		public FieldWithMetaString.FieldWithMetaStringBuilder getMarked() {
			return marked;
		}
		
		@Override
		public FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateMarked() {
			FieldWithMetaString.FieldWithMetaStringBuilder result;
			if (marked!=null) {
				result = marked;
			}
			else {
				result = marked = FieldWithMetaString.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("plain")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("plain")
		public String getPlain() {
			return plain;
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
		
		@RosettaAttribute("marked")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("marked")
		@Override
		public Holder.HolderBuilder setMarked(FieldWithMetaString _marked) {
			this.marked = _marked == null ? null : _marked.toBuilder();
			return this;
		}
		
		@Override
		public Holder.HolderBuilder setMarkedValue(String _marked) {
			this.getOrCreateMarked().setValue(_marked);
			return this;
		}
		
		@RosettaAttribute("plain")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("plain")
		@Override
		public Holder.HolderBuilder setPlain(String _plain) {
			this.plain = _plain == null ? null : _plain;
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
			if (marked!=null && !marked.prune().hasData()) marked = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getCoded()!=null) return true;
			if (getCodes()!=null && !getCodes().isEmpty()) return true;
			if (getMarked()!=null) return true;
			if (getPlain()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Holder.HolderBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Holder.HolderBuilder o = (Holder.HolderBuilder) other;
			
			merger.mergeRosetta(getCoded(), o.getCoded(), this::setCoded);
			merger.mergeRosetta(getCodes(), o.getCodes(), this::getOrCreateCodes);
			merger.mergeRosetta(getMarked(), o.getMarked(), this::setMarked);
			
			merger.mergeBasic(getPlain(), o.getPlain(), this::setPlain);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Holder _that = getType().cast(o);
		
			if (!Objects.equals(coded, _that.getCoded())) return false;
			if (!ListEquals.listEquals(codes, _that.getCodes())) return false;
			if (!Objects.equals(marked, _that.getMarked())) return false;
			if (!Objects.equals(plain, _that.getPlain())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (coded != null ? coded.hashCode() : 0);
			_result = 31 * _result + (codes != null ? codes.hashCode() : 0);
			_result = 31 * _result + (marked != null ? marked.hashCode() : 0);
			_result = 31 * _result + (plain != null ? plain.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "HolderBuilder {" +
				"coded=" + this.coded + ", " +
				"codes=" + this.codes + ", " +
				"marked=" + this.marked + ", " +
				"plain=" + this.plain +
			'}';
		}
	}
}
