package holdout.extractmetaelemdereffunction;

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
import holdout.extractmetaelemdereffunction.meta.OuterMeta;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * The holder.
 * @version 0.0.0
 */
@RosettaDataType(value="Outer", builder=Outer.OuterBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Outer", model="holdout", builder=Outer.OuterBuilderImpl.class, version="0.0.0")
public interface Outer extends RosettaModelObject {

	OuterMeta metaData = new OuterMeta();

	/*********************** Getter Methods  ***********************/
	FieldWithMetaString getCode();
	List<? extends FieldWithMetaString> getCodes();

	/*********************** Build Methods  ***********************/
	Outer build();
	
	Outer.OuterBuilder toBuilder();
	
	static Outer.OuterBuilder builder() {
		return new Outer.OuterBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Outer> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Outer> getType() {
		return Outer.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("code"), processor, FieldWithMetaString.class, getCode());
		processRosetta(path.newSubPath("codes"), processor, FieldWithMetaString.class, getCodes());
	}
	

	/*********************** Builder Interface  ***********************/
	interface OuterBuilder extends Outer, RosettaModelObjectBuilder {
		FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateCode();
		@Override
		FieldWithMetaString.FieldWithMetaStringBuilder getCode();
		FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateCodes(int index);
		@Override
		List<? extends FieldWithMetaString.FieldWithMetaStringBuilder> getCodes();
		Outer.OuterBuilder setCode(FieldWithMetaString code);
		Outer.OuterBuilder setCodeValue(String code);
		Outer.OuterBuilder addCodes(FieldWithMetaString codes);
		Outer.OuterBuilder addCodes(FieldWithMetaString codes, int idx);
		Outer.OuterBuilder addCodesValue(String codes);
		Outer.OuterBuilder addCodesValue(String codes, int idx);
		Outer.OuterBuilder addCodes(List<? extends FieldWithMetaString> codes);
		Outer.OuterBuilder setCodes(List<? extends FieldWithMetaString> codes);
		Outer.OuterBuilder addCodesValue(List<? extends String> codes);
		Outer.OuterBuilder setCodesValue(List<? extends String> codes);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("code"), processor, FieldWithMetaString.FieldWithMetaStringBuilder.class, getCode());
			processRosetta(path.newSubPath("codes"), processor, FieldWithMetaString.FieldWithMetaStringBuilder.class, getCodes());
		}
		

		Outer.OuterBuilder prune();
	}

	/*********************** Immutable Implementation of Outer  ***********************/
	class OuterImpl implements Outer {
		private final FieldWithMetaString code;
		private final List<? extends FieldWithMetaString> codes;
		
		protected OuterImpl(Outer.OuterBuilder builder) {
			this.code = ofNullable(builder.getCode()).map(f->f.build()).orElse(null);
			this.codes = ofNullable(builder.getCodes()).filter(_l->!_l.isEmpty()).map(list -> list.stream().filter(Objects::nonNull).map(f->f.build()).filter(Objects::nonNull).collect(ImmutableList.toImmutableList())).orElse(null);
		}
		
		@Override
		@RosettaAttribute("code")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("code")
		public FieldWithMetaString getCode() {
			return code;
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
		public Outer build() {
			return this;
		}
		
		@Override
		public Outer.OuterBuilder toBuilder() {
			Outer.OuterBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Outer.OuterBuilder builder) {
			ofNullable(getCode()).ifPresent(builder::setCode);
			ofNullable(getCodes()).ifPresent(builder::setCodes);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Outer _that = getType().cast(o);
		
			if (!Objects.equals(code, _that.getCode())) return false;
			if (!ListEquals.listEquals(codes, _that.getCodes())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (code != null ? code.hashCode() : 0);
			_result = 31 * _result + (codes != null ? codes.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Outer {" +
				"code=" + this.code + ", " +
				"codes=" + this.codes +
			'}';
		}
	}

	/*********************** Builder Implementation of Outer  ***********************/
	class OuterBuilderImpl implements Outer.OuterBuilder {
	
		protected FieldWithMetaString.FieldWithMetaStringBuilder code;
		protected List<FieldWithMetaString.FieldWithMetaStringBuilder> codes = new ArrayList<>();
		
		@Override
		@RosettaAttribute("code")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("code")
		public FieldWithMetaString.FieldWithMetaStringBuilder getCode() {
			return code;
		}
		
		@Override
		public FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateCode() {
			FieldWithMetaString.FieldWithMetaStringBuilder result;
			if (code!=null) {
				result = code;
			}
			else {
				result = code = FieldWithMetaString.builder();
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
		
		@RosettaAttribute("code")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("code")
		@Override
		public Outer.OuterBuilder setCode(FieldWithMetaString _code) {
			this.code = _code == null ? null : _code.toBuilder();
			return this;
		}
		
		@Override
		public Outer.OuterBuilder setCodeValue(String _code) {
			this.getOrCreateCode().setValue(_code);
			return this;
		}
		
		@RosettaAttribute("codes")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("codes")
		@Override
		public Outer.OuterBuilder addCodes(FieldWithMetaString _codes) {
			if (_codes != null) {
				this.codes.add(_codes.toBuilder());
			}
			return this;
		}
		
		@Override
		public Outer.OuterBuilder addCodes(FieldWithMetaString _codes, int idx) {
			getIndex(this.codes, idx, () -> _codes.toBuilder());
			return this;
		}
		
		@Override
		public Outer.OuterBuilder addCodesValue(String _codes) {
			this.getOrCreateCodes(-1).setValue(_codes);
			return this;
		}
		
		@Override
		public Outer.OuterBuilder addCodesValue(String _codes, int idx) {
			this.getOrCreateCodes(idx).setValue(_codes);
			return this;
		}
		
		@Override
		public Outer.OuterBuilder addCodes(List<? extends FieldWithMetaString> codess) {
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
		public Outer.OuterBuilder setCodes(List<? extends FieldWithMetaString> codess) {
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
		public Outer.OuterBuilder addCodesValue(List<? extends String> codess) {
			if (codess != null) {
				for (final String toAdd : codess) {
					this.addCodesValue(toAdd);
				}
			}
			return this;
		}
		
		@Override
		public Outer.OuterBuilder setCodesValue(List<? extends String> codess) {
			this.codes.clear();
			if (codess != null) {
				codess.forEach(this::addCodesValue);
			}
			return this;
		}
		
		@Override
		public Outer build() {
			return new Outer.OuterImpl(this);
		}
		
		@Override
		public Outer.OuterBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Outer.OuterBuilder prune() {
			if (code!=null && !code.prune().hasData()) code = null;
			codes = codes.stream().filter(b->b!=null).<FieldWithMetaString.FieldWithMetaStringBuilder>map(b->b.prune()).filter(b->b.hasData()).collect(Collectors.toList());
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getCode()!=null) return true;
			if (getCodes()!=null && !getCodes().isEmpty()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Outer.OuterBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Outer.OuterBuilder o = (Outer.OuterBuilder) other;
			
			merger.mergeRosetta(getCode(), o.getCode(), this::setCode);
			merger.mergeRosetta(getCodes(), o.getCodes(), this::getOrCreateCodes);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Outer _that = getType().cast(o);
		
			if (!Objects.equals(code, _that.getCode())) return false;
			if (!ListEquals.listEquals(codes, _that.getCodes())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (code != null ? code.hashCode() : 0);
			_result = 31 * _result + (codes != null ? codes.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "OuterBuilder {" +
				"code=" + this.code + ", " +
				"codes=" + this.codes +
			'}';
		}
	}
}
